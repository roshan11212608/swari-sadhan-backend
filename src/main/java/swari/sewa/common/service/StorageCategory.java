package swari.sewa.common.service;

/**
 * Image/object categories used to build predictable R2 object key prefixes.
 */
public enum StorageCategory {

    VEHICLE("vehicles"),
    USER("users"),
    SHOP("shops"),
    EXPENSE("expenses"),
    SHOP_REGISTRATION("shopreg"),
    MISC("misc");

    private final String prefix;

    StorageCategory(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
