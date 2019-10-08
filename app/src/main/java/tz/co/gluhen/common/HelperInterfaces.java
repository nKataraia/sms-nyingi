package tz.co.gluhen.common;

public class HelperInterfaces{
    public interface Consumer<T> { void take(T t);}
    public interface BiConsumer<T,U> { void take(T t,U u);}
    public interface Changer<I,O> { O change(I t);}
    public interface BiFunction<I,O,T> { T change(I t,O o);}
}
