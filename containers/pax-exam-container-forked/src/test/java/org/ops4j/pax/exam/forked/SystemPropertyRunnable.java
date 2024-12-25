package org.ops4j.pax.exam.forked;

/*package */ abstract class SystemPropertyRunnable implements Runnable {

    private String name;
    private String value;

    public SystemPropertyRunnable(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    @Override
    public void run() {
        String existingValue = null;
        try {
            existingValue = System.setProperty(name, value);
            doRun();
        } finally {
            if (existingValue == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, existingValue);
            }
        }
    }

    protected abstract void doRun();
}