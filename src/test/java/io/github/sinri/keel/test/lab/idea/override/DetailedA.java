package io.github.sinri.keel.test.lab.idea.override;

public class DetailedA extends SimpleA {
    @Override
    public void m1() {
        System.out.println("DetailedA.m1");
    }

    @Override
    public void m2() {
        System.out.println("DetailedA.m2");
    }
}
