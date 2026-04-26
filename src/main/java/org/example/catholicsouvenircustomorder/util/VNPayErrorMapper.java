package org.example.catholicsouvenircustomorder.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map VNPay error codes to Vietnamese error messages
 * Based on VNPay API documentation
 * Requirements: 12.1
 */
public class VNPayErrorMapper {
    
    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();
    
    static {
        // Success
        ERROR_MESSAGES.put("00", "Giao dịch thành công");
        
        // Common errors
        ERROR_MESSAGES.put("01", "Giao dịch đang chờ xử lý");
        ERROR_MESSAGES.put("02", "Merchant không tồn tại");
        ERROR_MESSAGES.put("03", "Dữ liệu không hợp lệ");
        ERROR_MESSAGES.put("04", "Giao dịch không tồn tại");
        ERROR_MESSAGES.put("05", "Giao dịch đã được hoàn tiền");
        ERROR_MESSAGES.put("06", "Giao dịch đang được xử lý");
        ERROR_MESSAGES.put("07", "Giao dịch bị nghi ngờ gian lận");
        ERROR_MESSAGES.put("08", "Giao dịch bị từ chối bởi ngân hàng");
        ERROR_MESSAGES.put("09", "Giao dịch chưa hoàn tất");
        ERROR_MESSAGES.put("10", "Giao dịch không được phép");
        ERROR_MESSAGES.put("11", "Giao dịch đã hết hạn");
        ERROR_MESSAGES.put("12", "Thẻ/Tài khoản bị khóa");
        
        // Authentication errors
        ERROR_MESSAGES.put("13", "Xác thực không chính xác");
        ERROR_MESSAGES.put("14", "Thông tin thẻ không chính xác");
        ERROR_MESSAGES.put("15", "Ngân hàng phát hành thẻ không hỗ trợ giao dịch");
        ERROR_MESSAGES.put("16", "Thông tin khách hàng chưa xác thực");
        ERROR_MESSAGES.put("17", "Ngân hàng từ chối giao dịch");
        ERROR_MESSAGES.put("18", "Giao dịch bị hủy bởi khách hàng");
        ERROR_MESSAGES.put("19", "Giao dịch không thành công do lỗi hệ thống");
        ERROR_MESSAGES.put("20", "Giao dịch không được phép trong khung thời gian này");
        ERROR_MESSAGES.put("21", "Số tiền giao dịch không hợp lệ");
        ERROR_MESSAGES.put("22", "Thông tin merchant không hợp lệ");
        ERROR_MESSAGES.put("23", "Giao dịch không được hỗ trợ");
        ERROR_MESSAGES.put("24", "Giao dịch bị hủy");
        ERROR_MESSAGES.put("25", "Giao dịch không thành công do lỗi kết nối");
        
        // Balance and limit errors
        ERROR_MESSAGES.put("51", "Tài khoản không đủ số dư");
        ERROR_MESSAGES.put("52", "Tài khoản bị đóng băng");
        ERROR_MESSAGES.put("53", "Tài khoản không tồn tại");
        ERROR_MESSAGES.put("54", "Tài khoản đã hết hạn");
        ERROR_MESSAGES.put("55", "Thông tin tài khoản không chính xác");
        ERROR_MESSAGES.put("56", "Tài khoản chưa được kích hoạt");
        ERROR_MESSAGES.put("57", "Giao dịch vượt quá hạn mức ngày");
        ERROR_MESSAGES.put("58", "Giao dịch vượt quá hạn mức tháng");
        ERROR_MESSAGES.put("59", "Giao dịch vượt quá hạn mức giao dịch");
        ERROR_MESSAGES.put("60", "Tài khoản đã vượt quá số lần giao dịch cho phép");
        ERROR_MESSAGES.put("61", "Ngân hàng không hỗ trợ giao dịch này");
        ERROR_MESSAGES.put("62", "Giao dịch bị từ chối do chính sách bảo mật");
        ERROR_MESSAGES.put("63", "Thông tin xác thực không đúng");
        ERROR_MESSAGES.put("64", "Giao dịch bị từ chối do nghi ngờ gian lận");
        ERROR_MESSAGES.put("65", "Tài khoản đã vượt quá giới hạn giao dịch");
        ERROR_MESSAGES.put("66", "Giao dịch không được phép vào thời điểm này");
        ERROR_MESSAGES.put("67", "Giao dịch bị từ chối do vi phạm quy định");
        ERROR_MESSAGES.put("68", "Giao dịch không thành công do lỗi kỹ thuật");
        ERROR_MESSAGES.put("69", "Giao dịch bị từ chối do lý do bảo mật");
        ERROR_MESSAGES.put("70", "Giao dịch không được hỗ trợ bởi ngân hàng");
        ERROR_MESSAGES.put("71", "Giao dịch bị từ chối do thông tin không đầy đủ");
        ERROR_MESSAGES.put("72", "Giao dịch bị từ chối do lỗi xử lý");
        ERROR_MESSAGES.put("73", "Giao dịch không thành công do lỗi mạng");
        ERROR_MESSAGES.put("74", "Giao dịch bị từ chối do chính sách ngân hàng");
        ERROR_MESSAGES.put("75", "Ngân hàng thanh toán đang bảo trì");
        ERROR_MESSAGES.put("76", "Giao dịch không thành công do quá tải hệ thống");
        ERROR_MESSAGES.put("77", "Giao dịch bị từ chối do lỗi cấu hình");
        ERROR_MESSAGES.put("78", "Giao dịch không được phép do hạn chế địa lý");
        ERROR_MESSAGES.put("79", "Giao dịch vượt quá số tiền cho phép");
        ERROR_MESSAGES.put("80", "Giao dịch bị từ chối do lỗi xác thực");
        
        // Refund specific errors
        ERROR_MESSAGES.put("91", "Không tìm thấy giao dịch");
        ERROR_MESSAGES.put("92", "Giao dịch không hợp lệ để hoàn tiền");
        ERROR_MESSAGES.put("93", "Số tiền hoàn vượt quá số tiền gốc");
        ERROR_MESSAGES.put("94", "Yêu cầu trùng lặp");
        ERROR_MESSAGES.put("95", "Giao dịch đã quá hạn hoàn tiền");
        ERROR_MESSAGES.put("96", "Giao dịch hoàn tiền không được phép");
        ERROR_MESSAGES.put("97", "Chữ ký không hợp lệ");
        ERROR_MESSAGES.put("98", "Hệ thống đang bảo trì");
        ERROR_MESSAGES.put("99", "Lỗi không xác định");
    }
    
    /**
     * Map VNPay error code to Vietnamese error message
     * 
     * @param code VNPay response code
     * @return Vietnamese error message
     */
    public static String mapErrorCode(String code) {
        if (code == null || code.isEmpty()) {
            return "Mã lỗi không hợp lệ";
        }
        return ERROR_MESSAGES.getOrDefault(code, "Lỗi không xác định: " + code);
    }
    
    /**
     * Check if the response code indicates success
     * 
     * @param code VNPay response code
     * @return true if successful, false otherwise
     */
    public static boolean isSuccess(String code) {
        return "00".equals(code);
    }
    
    /**
     * Check if the response code indicates a retryable error
     * 
     * @param code VNPay response code
     * @return true if the error is retryable, false otherwise
     */
    public static boolean isRetryable(String code) {
        // Retryable errors: processing, maintenance, timeout
        return "06".equals(code) || "75".equals(code) || "98".equals(code);
    }
    
    /**
     * Get all error code mappings
     * 
     * @return Map of error codes to messages
     */
    public static Map<String, String> getAllErrorMappings() {
        return new HashMap<>(ERROR_MESSAGES);
    }
}
