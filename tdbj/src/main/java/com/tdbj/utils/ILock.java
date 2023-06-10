package com.tdbj.utils;

public interface ILock {

    boolean tryLock(long timeoutsec);

    void unLock();
}
