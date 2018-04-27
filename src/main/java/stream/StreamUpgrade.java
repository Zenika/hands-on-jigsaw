package stream;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class StreamUpgrade {

    public static void main(String[] args) {
        Random random = new Random(System.currentTimeMillis());
        List<Integer> integerList = random.ints(10, 0,10).sorted().boxed()
                .collect(Collectors.toList());

        System.out.println(integerList.stream().map(String::valueOf).collect(Collectors.joining(";")));


        System.out.println("TakeWhile : " +
                integerList.stream()
                        .sorted()
                        .takeWhile(i -> i <= 5)
                        .map(i -> i.toString()).collect(Collectors.joining(";")));

        System.out.println("DropWhile : " +
                integerList.stream()
                        .sorted()
                        .dropWhile(i -> i <= 5)
                        .map(i -> i.toString()).collect(Collectors.joining(";")));


        try {
            Stream.of(null).map(nul -> nul.toString());
            System.err.println("OH NO");
        } catch (NullPointerException npe) {
            System.out.println("This might fail");
        }
        System.out.println("But now this must not fail");
        Stream.ofNullable(null).map(nul -> nul.toString());
        System.out.println("Oh yeah");

    }
}
