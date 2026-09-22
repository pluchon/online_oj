package cn.nuonuoya.friend.converter;

import cn.nuonuoya.friend.domain.TbUserCodeDraft;
import cn.nuonuoya.friend.vo.CodeDraftVO;

// 代码草稿转换
public class CodeDraftConverter {

    private CodeDraftConverter() {
    }

    // 草稿实体转视图（最后保存时间取更新时间，未更新过取创建时间）
    public static CodeDraftVO toVO(TbUserCodeDraft draft) {
        CodeDraftVO vo = new CodeDraftVO();
        vo.setCode(draft.getCode());
        vo.setSavedTime(draft.getUpdateTime() != null ? draft.getUpdateTime() : draft.getCreateTime());
        return vo;
    }
}
