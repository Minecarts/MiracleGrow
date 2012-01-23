package com.minecarts.miraclegrow;

public class Stopwatch {
    protected long elapsed = 0;
    protected long start = 0;
    
    public Stopwatch start() {
        start = System.currentTimeMillis();
        return this;
    }
    
    public Stopwatch stop() {
        if(start > 0) {
            elapsed += System.currentTimeMillis() - start;
            start = 0;
        }
        return this;
    }
    
    public long elapsed() {
        return elapsed;
    }
}
