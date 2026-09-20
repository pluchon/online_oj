package cn.nuonuoya.friend.controller;

import cn.nuonuoya.common.controller.BaseController;
import cn.nuonuoya.common.domain.OJResult;
import cn.nuonuoya.common.domain.PageQuery;
import cn.nuonuoya.common.domain.TableDataResult;
import cn.nuonuoya.friend.service.MessageService;
import cn.nuonuoya.friend.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// C端站内消息控制器
@Validated
@RestController
@RequestMapping("/message")
@Tag(name = "C端消息中心API")
public class MessageController extends BaseController {

    @Autowired
    private MessageService messageService;

    /** 分页查询当前用户站内消息列表 */
    @GetMapping("/list")
    @Operation(summary = "消息列表", description = "分页查询当前登录用户的站内消息列表")
    public TableDataResult<MessageVO> list(PageQuery pageQuery) {
        return messageService.list(pageQuery);
    }

    /** 查询当前用户未读消息数量 */
    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数", description = "获取当前登录用户的未读消息总数")
    public OJResult<Integer> unreadCount() {
        int count = messageService.getUnreadCount();
        return OJResult.ok(count);
    }

    /** 标记单条消息为已读 */
    @PutMapping("/read")
    @Operation(summary = "标记消息已读", description = "将指定的一条消息标记为已读状态")
    public OJResult<Void> readMessage(@RequestParam("messageId") Long messageId) {
        messageService.readMessage(messageId);
        return OJResult.ok();
    }

    /** 一键全部标记为已读 */
    @PutMapping("/read/all")
    @Operation(summary = "全部标记已读", description = "将当前登录用户的所有未读消息一键置为已读")
    public OJResult<Void> readAll() {
        messageService.readAll();
        return OJResult.ok();
    }
}
