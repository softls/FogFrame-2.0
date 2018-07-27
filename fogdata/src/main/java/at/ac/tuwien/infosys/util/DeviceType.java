package at.ac.tuwien.infosys.util;

/**
 * Created by Kevin Bachmann on 02/11/2016.
 */
public enum DeviceType {
    FOG_CELL("fog_cell"), FOG_NODE("fog_node"), FOG_CONTROLLER("fog_controller"),
    IOT_DEVICE("iot_device"), CLOUD_SERVICE("cloud_service");


    private String value;
    DeviceType(final String key) {
        this.value = key;
    }
    public String getValue() {
        return value;
    }
    @Override
    public String toString() {
        return this.getValue();
    }
}

