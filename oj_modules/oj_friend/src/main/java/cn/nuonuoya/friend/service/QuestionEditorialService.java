package cn.nuonuoya.friend.service;

import cn.nuonuoya.friend.vo.QuestionEditorialVO;

// 题目官方题解查询服务
public interface QuestionEditorialService {

    // 查询题目的官方题解：题目正在进行中的竞赛里使用时拒绝，没有题解时返回 null
    QuestionEditorialVO getEditorial(Long questionId);
}
