package javax.cache.implementation;

import javax.cache.CacheConfiguration;

/**
 * @author ycosmado
 * @since 1.0
 */
public class RICacheConfiguration implements CacheConfiguration {
    private boolean readThrough;
    private boolean writeThrough;
    private boolean storeByValue;

    private RICacheConfiguration(boolean readThrough, boolean writeThrough, boolean storeByValue) {
        setReadThrough(readThrough);
        setWriteThrough(writeThrough);
        setStoreByValue(storeByValue);
    }

    public boolean isReadThrough() {
        return readThrough;
    }

    public void setReadThrough(boolean readThrough) {
        this.readThrough = readThrough;
    }

    public boolean isWriteThrough() {
        return writeThrough;
    }

    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    public boolean isStoreByValue() {
        return storeByValue;
    }

    public void setStoreByValue(boolean storeByValue) {
        this.storeByValue = storeByValue;
    }

    public static class Builder {
        private boolean readThrough;
        private boolean writeThrough;
        private boolean storeByValue;

        public Builder setReadThrough(boolean readThrough) {
            this.readThrough = readThrough;
            return this;
        }

        public Builder setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
            return this;
        }

        public Builder setStoreByValue(boolean storeByValue) {
            this.storeByValue = storeByValue;
            return this;
        }

        public RICacheConfiguration build() {
            return new RICacheConfiguration(readThrough, writeThrough, storeByValue);
        }
    }
}
