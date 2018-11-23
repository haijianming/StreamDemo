package com.llvision.libjava;

import static java.lang.Thread.sleep;

public class MyClass {
    static class methodA{
         static int  code=0;
        public  static synchronized int getcode(){
            code++;
            return code;
        }
    }
    class methodB{

    }
    public static void main(String[] arge){
        System.out.println("test");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    System.out.println("thread1:"+methodA.getcode());
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("thread2:"+methodA.getcode());
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
