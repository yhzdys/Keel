package io.github.sinri.keel.test.lab.idea.override;

public class SimpleA implements A {

    @Override
    public void m1() {
        System.out.println("SimpleA.m1");
    }

    public void m2() {
        System.out.println("SimpleA.m2");
    }
}
