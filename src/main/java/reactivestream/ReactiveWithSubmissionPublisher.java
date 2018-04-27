package reactivestream;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class ReactiveWithSubmissionPublisher {

    public static void main(String[] args) {

        System.out.println("Start");
        final RandomNumberPublisher<Integer> integerRandomNumberPublisher = new RandomNumberPublisher<>();



        integerRandomNumberPublisher.subscribe(new MySubscriber());


        integerRandomNumberPublisher.push(0, 10, 100);
        try {

            Thread.currentThread().sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    static class RandomNumberPublisher<Integer> extends SubmissionPublisher {
        final Random random = new Random(System.currentTimeMillis());

        public void push(int min, int max, int limit) {
            System.out.println("Push");
            Random random = new Random(System.currentTimeMillis());
            final Stream<java.lang.Integer> stream = random.ints(limit,min,max) .boxed();
            stream.forEach(i -> submit(i));
        }

    }


    static class MySubscriber implements Flow.Subscriber<Integer> {


        private Flow.Subscription subscription;
        final private AtomicInteger nb = new AtomicInteger(0);

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(10);
        }

        @Override
        public void onNext(Integer item) {
            final int i = nb.getAndIncrement();
            System.out.printf("%d\t=>\t%d\n", i , item);
            if (i%10 == 0)
                subscription.request(10);
//            if (i >= 50)
//                subscription.cancel();
        }


        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
            System.out.println(nb.get());
        }
    }


}
