package samples3;

@AnnotationExample.MultiValueAnnotation(value2 = 2, option1 = false, option2 = false)
public class AnnotationExample {
    @Deprecated
    @SingleValueAnnotation("a") // only recognized as valid when parameter is named "value" ...
    public void exampleMethodWithAnnotations(@SingleValueAnnotation(value = "a" + "b") int parameter) {

    }

    public @interface SingleValueAnnotation {
        String value(); // ... try it out, change it to "val" for example (see above)
    }

    public @interface MultiValueAnnotation {
        int value1() default 1;

        int value2();

        boolean option1() default true;

        boolean option2();
    }
}
