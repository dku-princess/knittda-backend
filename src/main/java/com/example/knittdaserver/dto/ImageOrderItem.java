package com.example.knittdaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수정(PUT) 시 클라가 보내는 "최종 이미지 표시 순서" 배열의 한 요소.
 *
 * <p>배열의 순서가 곧 최종 표시 순서이며, 서버는 이 배열 위치로 imageOrder(1-base)를 재부여한다.
 * <ul>
 *   <li>{@code type = "existing"} : 기존 이미지. {@code id} 로 식별.</li>
 *   <li>{@code type = "new"}      : 신규 업로드 이미지. {@code index} 로 multipart {@code files[index]} 와 매핑.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageOrderItem {

    public static final String TYPE_EXISTING = "existing";
    public static final String TYPE_NEW = "new";

    /** "existing" | "new" */
    private String type;

    /** type=existing 일 때 기존 이미지 id */
    private Long id;

    /** type=new 일 때 multipart files 배열의 인덱스(0-base) */
    private Integer index;

    private boolean typeUnset() {
        return type == null || type.isBlank();
    }

    /**
     * 신규 업로드 이미지 여부.
     * type=="new" 이거나, (구버전 호환) type 미지정 + index만 존재하는 경우.
     */
    public boolean isNew() {
        if (TYPE_NEW.equalsIgnoreCase(type)) {
            return true;
        }
        return typeUnset() && index != null && id == null;
    }

    /**
     * 기존 이미지 여부.
     * type=="existing" 이거나, (구버전 호환) type 미지정 + id가 존재하는 경우.
     * 구버전 클라가 보내는 {id, imageUrl, imageOrder} shape도 이 분기로 흡수된다.
     */
    public boolean isExisting() {
        if (TYPE_EXISTING.equalsIgnoreCase(type)) {
            return true;
        }
        return typeUnset() && id != null;
    }
}
