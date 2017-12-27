package samples3;

@AnnotationExample.MultiValueAnnotation(value2 = 2, option1 = false, option2 = false)
public class AnnotationExample {

    public @interface MultiValueAnnotation {
        int value1() default 1;
        int value2();
        boolean option1() default true;
        boolean option2();
    }
}
