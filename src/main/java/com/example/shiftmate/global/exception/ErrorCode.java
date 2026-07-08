package com.example.shiftmate.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게 정보를 찾을 수 없습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "시작 시간은 종료 시간보다 빨라야 합니다."),
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "매장 수정 권한이 없습니다."),

    // Store Image
    STORE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게 이미지를 찾을 수 없습니다."),
    STORE_IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "가게 이미지 업로드에 실패했습니다."),

    // Store Member
    STORE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "매장 멤버 정보를 찾을 수 없습니다."),
    STORE_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 매장에 등록된 멤버입니다."),
    STORE_MEMBER_STORE_ID_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 매장 ID가 해당 멤버의 매장 ID와 일치하지 않습니다."),
    STORE_MEMBER_USER_ID_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 사용자 ID가 해당 멤버의 사용자 ID와 일치하지 않습니다."),
    STORE_MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "매장 멤버 관리 권한이 없습니다."),

    //Preference
    PREFERENCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 선호 시간이 존재합니다."),
    PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "선호 시간이 존재하지 않습니다."),


    //Template
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND,"템플릿이 존재하지 않습니다."),
    TEMPLATE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가게 템플릿이 생성되어 있습니다."),
    TYPE_NOT_FOUND(HttpStatus.NOT_FOUND,"타입이 존재하지 않습니다."),

    //Token
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "토큰 서명이 유효하지 않습니다."),
    MALFORMED_TOKEN(HttpStatus.BAD_REQUEST, "토큰 형식이 올바르지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "지원하지 않는 토큰 형식입니다."),
    EMPTY_TOKEN(HttpStatus.BAD_REQUEST, "토큰이 존재하지 않습니다."),

    //Kakao Token
    OAUTH_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "토큰 발행 실패"),
    OAUTH_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "소셜유저 정보조회 실패"),

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // User
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // SignUp
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다."),

    // SignUp Email Verification
    SIGNUP_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    SIGNUP_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "이메일 인증 요청을 찾을 수 없습니다."),
    SIGNUP_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "이메일 인증 코드가 만료되었습니다."),
    SIGNUP_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "이메일 인증 코드가 일치하지 않습니다."),

    // Password Change
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "새 비밀번호 확인이 일치하지 않습니다."),
    NEW_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "새 비밀번호는 기존 비밀번호와 달라야 합니다."),

    // Reset Password
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 재설정 링크입니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "재설정 링크가 만료되었습니다."),

    // File Upload
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다."),
    DOCUMENT_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "문서 업로드에 실패했습니다."),
    DOCUMENT_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "문서 다운로드에 실패했습니다."),

    // ShiftAssignment
    WEEK_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 주에는 이미 시간표가 생성되어 있습니다."),
    NOT_MONDAY_START_DATE(HttpStatus.BAD_REQUEST, "시작 요일은 월요일이어야 합니다."),
    SHIFT_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "배정된 스케줄을 찾을 수 없습니다."),

    // Attendance
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출근 기록을 찾을 수 없습니다."),
    ATTENDANCE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 퇴근처리된 스케줄입니다."),
    INVALID_OTP(HttpStatus.UNAUTHORIZED, "OTP 번호가 일치하지 않거나 만료되었습니다."),
    STORE_MISMATCH(HttpStatus.BAD_REQUEST, "해당 매장의 스케줄이 아닙니다."),
    NOT_CLOCK_IN_TIME(HttpStatus.BAD_REQUEST, "해당 스케줄의 출근 처리 가능 시간이 아닙니다."),
    NOT_CLOCK_OUT_TIME(HttpStatus.BAD_REQUEST, "해당 스케줄의 퇴근 처리 가능 시간이 아닙니다."),
    TOO_FAST_CLOCK_OUT(HttpStatus.BAD_REQUEST, "출근 처리 5분 후 퇴근 처리가 가능합니다."),

    // Substitute
    SUBSTITUTE_REQ_NOT_FOUND(HttpStatus.NOT_FOUND, "대타 요청을 찾을 수 없습니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 대타 요청이 진행 중인 스케줄입니다."),
    NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "취소할 수 없는 상태입니다."),
    PAST_SCHEDULE_CANNOT_REQUEST(HttpStatus.BAD_REQUEST, "이미 지난 스케줄에는 대타 요청을 할 수 없습니다."),
    TOO_LATE_FOR_SUBSTITUTE(HttpStatus.BAD_REQUEST, "대타 요청은 근무 시작 최소 24시간 전까지만 가능합니다."),

    //Application
    ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 지원한 요청입니다."),
    CANNOT_APPLY(HttpStatus.BAD_REQUEST, "요청에 지원할 수 없는 상태입니다."),
    CANNOT_SELECT(HttpStatus.BAD_REQUEST, "승인할 수 없는 지원입니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원을 찾을 수 없습니다."),
    NOT_SUBSTITUTE_APPLICATION(HttpStatus.FORBIDDEN, "해당 요청의 지원이 아닙니다."),
    DEPARTMENT_MISMATCH(HttpStatus.BAD_REQUEST, "다른 부서의 근무에는 대타 지원을 할 수 없습니다."),
    DUPLICATE_SHIFT(HttpStatus.CONFLICT, "이미 배정된 스케줄과 시간이 겹쳐 대타 지원이 불가능합니다."),
    PAST_SCHEDULE_CANNOT_APPROVE(HttpStatus.BAD_REQUEST, "이미 지난 스케줄의 대타 지원은 승인할 수 없습니다."),

    // Bizno API
    BIZNO_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "사업자 번호 조회 API 호출에 실패했습니다."),
    INVALID_BIZNO(HttpStatus.BAD_REQUEST, "유효하지 않은 사업자 번호입니다."),

    // OpenShift
    PAST_DATE_CANNOT_REQUEST(HttpStatus.BAD_REQUEST, "이미 지난 날짜에는 오픈시프트를 생성할 수 없습니다."),
    OPEN_SHIFT_NOT_FOUND(HttpStatus.BAD_REQUEST, "오픈시프트를 찾을 수 없습니다."),

    // Chat
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_PARTICIPANT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방의 참여자가 아닙니다.");

    private final HttpStatus status;
    private final String message;
}
