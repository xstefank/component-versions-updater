package io.xstefank.model.yml;

public class Component {

    public String groupId;
    public String version;

    public Component() {
    }

    private Component(String groupId, String version) {
        this.groupId = groupId;
        this.version = version;
    }

    @Override
    public String toString() {
        return "Component{" +
            "groupId='" + groupId + '\'' +
            ", version='" + version + '\'' +
            '}';
    }
}
