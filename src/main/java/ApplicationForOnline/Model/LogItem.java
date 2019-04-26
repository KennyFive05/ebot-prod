package ApplicationForOnline.Model;

import lombok.Getter;
import lombok.Setter;

public class LogItem {
    @Getter
    @Setter
    private boolean isSuccess;
    @Getter
    @Setter
    private String description;
    @Getter
    @Setter
    private String frnMsgID;
    @Getter
    @Setter
    private String rqXML;
    @Getter
    @Setter
    private String rsXML;
}
