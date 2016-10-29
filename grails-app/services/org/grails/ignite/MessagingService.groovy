package org.grails.ignite

import org.apache.ignite.IgniteMessaging
import org.apache.ignite.cache.CacheAtomicityMode
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.lang.IgniteCallable
import org.springframework.beans.factory.InitializingBean

/**
 * Support point-to-point and topic-based messaging throughout the cluster
 */
class MessagingService implements InitializingBean {

    public static final String QUEUE_DESTINATION_CACHE_NAME = '__queueDestinationCache'
    public static final long TIMEOUT = 30000
    static transactional = false

    def grid
    // a local cache of receivers that will be assigned messages upon arrival at this node. It's
    // not always feasable to serialize message receivers
    //private Map localReceiverCache = new HashMap<String, MessageReceiver>()

    @Override
    public void afterPropertiesSet() {
        // for testing where no grid is available, need to be able to initialize this bean
        if (grid == null) {
            //           throw new RuntimeException("Can't configure messaging, no grid is configured")
            log.warn "Can't configure messaging, no grid is configured"
        } else {
            CacheConfiguration<String, MessageReceiver> cacheConf = new CacheConfiguration<>();
            cacheConf.setName(QUEUE_DESTINATION_CACHE_NAME)
            cacheConf.setCacheMode(CacheMode.PARTITIONED)
            cacheConf.setAtomicityMode(CacheAtomicityMode.ATOMIC)
            cacheConf.setBackups(0)
            grid.getOrCreateCache(cacheConf)
            log.debug "afterPropertiesSet --> configured cache ${cacheConf}"
        }
    }

    /**
     * Send a message to a destination. This method emulates the old Grails JMS method of sending messages, e.g.,
     * <pre> sendMessage(queue:'queue_name', message) </pre>
     * <p>or</p>
     * <pre> sendMessage(topic: 'topic_name', message)
     */
    def sendMessage(destination, message) {
        log.debug "sendMessage(${destination},${message})"
        if (!(destination instanceof Map)) {
            throw new RuntimeException("Message destination must be of the form [type:name], e.g., [queue:'myQueue']")
        }

        if (destination.queue) {
            def queueName = destination.queue
            // execute the listener on the receiving node
            grid.compute().call(new IgniteCallable<Object>() {
                @Override
                public Object call() throws Exception {
                    // get a listener for this destination
                    def receiver = (MessageReceiver) grid.cache(QUEUE_DESTINATION_CACHE_NAME).get(queueName)
                    if (receiver == null) {
//                        throw new RuntimeException("No receiver configured for queue ${destination.queue}")
                        // suppress warnings?
                        log.warn "No receiver configured for queue ${destination.queue}"
                    } else {
                        receiver.receive(destination, message)
                    }
                }
            });
        }

        if (destination.topic) {
            log.debug "sending to topic: ${destination.topic}, ${message}, with timeout=${TIMEOUT}"
            IgniteMessaging rmtMsg = grid.message();
            rmtMsg.sendOrdered(destination.topic, message, TIMEOUT)
        }
    }

    def registerReceiver(destination, MessageReceiver receiver) {
        log.debug "registerListener(${destination},${receiver})"
        if (!(destination instanceof Map)) {
            throw new RuntimeException("Message destination must be of the form [type:name], e.g., [queue:'myQueue']")
        }

        if (destination.queue) {
            grid.cache(QUEUE_DESTINATION_CACHE_NAME).put(destination.queue, receiver)
        }

        if (destination.topic) {
            IgniteMessaging rmtMsg = grid.message();
            def topicName = destination.topic
            rmtMsg.remoteListen(topicName, new IgniteMessagingRemoteListener(receiver, destination));
        }
    }
}
