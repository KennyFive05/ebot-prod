package ApplicationForOnline.Model;

import lombok.Getter;
import lombok.Setter;

public class UpdateModel implements Cloneable {
    @Getter
    @Setter
    private String version;
    @Getter
    @Setter
    private String sheet;
    @Getter
    @Setter
    private String fromExecl;
    @Getter
    @Setter
    private String fromPath;
    @Getter
    @Setter
    private String toPath;
    @Getter
    @Setter
    private String publishPath;
    @Getter
    @Setter
    private boolean isCopy;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
