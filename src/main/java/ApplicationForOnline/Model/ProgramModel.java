package ApplicationForOnline.Model;

import lombok.Getter;
import lombok.Setter;

public class ProgramModel implements Cloneable {

    /**
     * 路徑
     */
    @Getter
    @Setter
    private String path;

    /**
     * 名稱
     */
    @Getter
    @Setter
    private String name;

    /**
     * 異動
     */
    @Getter
    @Setter
    private String status;

    /**
     * Execl 「換版原因」序號
     */
    @Getter
    @Setter
    private int execlNo;

    /**
     * 錯誤訊息
     */
    @Getter
    @Setter
    private String ErrorMessage;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}