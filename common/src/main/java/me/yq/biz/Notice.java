package me.yq.biz;

/**
 * 通知类，服务端很多时候需要主动向客户端推送一些消息<br/>
 * 比如通知新好友、新通知、新闻、广告
 * @author yq
 * @version v1.0 2023-03-16 17:44
 */
public class Notice {
    private final long acceptId;

    private final String noticeTitle;

    private final String noticeContent;

    public Notice(long acceptId, String noticeTitle, String noticeContent) {
        this.acceptId = acceptId;
        this.noticeTitle = noticeTitle;
        this.noticeContent = noticeContent;
    }

    public long getAcceptId() {
        return acceptId;
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public String getNoticeContent() {
        return noticeContent;
    }
}
