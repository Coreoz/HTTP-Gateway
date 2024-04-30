package com.coreoz.http.publisher;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Utility function for {@link PublisherPeeker}.
 */
@Slf4j
public class PublisherPeekers {
    /**
     * The number of body parts to read each time before verifying that
     * {@link PublisherPeeker#isPeekingFinished()}
     */
    private static final int SIZE_OF_BODY_PARTS_TO_READ = 100_000;

    /**
     * Consume asynchronously a {@link PublisherPeeker} to be able to make the peeking publisher see the content
     * when the publisher is not forwarded.<br>
     * This is especially useful in case of a remote Server Error that we don't want to forward.<br>
     * <br>
     * The peeking process will stop as soon as the base publisher is finished or if the peeking publisher has peeked
     * enough bytes.
     * @param publisherToConsume The {@link PublisherPeeker} publisher to consume
     * @throws IllegalArgumentException if the publisherToConsume is not an instance of {@link PublisherPeeker}
     */
    public static void consume(@NotNull Publisher<?> publisherToConsume) {
        if (!(publisherToConsume instanceof PublisherPeeker<?> peekerPublisherToConsume)) {
            // Cannot consume publisher that is not an instance of PublisherPeeker
            throw new IllegalArgumentException("publisherToConsume must be an instance of PublisherPeeker");
        }
        SubscriptionHolder subscriptionHolder = new SubscriptionHolder();
        peekerPublisherToConsume.subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionHolder.setSubscription(s);
                s.request(SIZE_OF_BODY_PARTS_TO_READ);
            }

            @Override
            public void onNext(Object o) {
                if (subscriptionHolder.getSubscription() == null) {
                    logger.warn("Trying to read subscription whereas it does not yet exist");
                    return;
                }
                if (peekerPublisherToConsume.isPeekingFinished()) {
                    logger.trace("Finished reading body");
                    subscriptionHolder.getSubscription().cancel();
                    return;
                }
                logger.trace("Requesting content from subscription");
                subscriptionHolder.getSubscription().request(SIZE_OF_BODY_PARTS_TO_READ);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error reading the error subscription", t);
            }

            @Override
            public void onComplete() {
                // finished to read the error body data
            }
        });
    }

    @Getter
    @Setter
    private static class SubscriptionHolder {
        private Subscription subscription;
    }
}
