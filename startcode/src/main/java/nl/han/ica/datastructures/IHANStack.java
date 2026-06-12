package nl.han.ica.datastructures;

public interface IHANStack<T> {
    void push(T value);
    T pop();
    T peek();
    boolean isEmpty();
    int getSize();
}