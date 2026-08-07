package swari.sewa.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.employee.entity.Attendance;
import swari.sewa.module.employee.entity.Employee;
import swari.sewa.module.employee.entity.AdvancePayment;
import swari.sewa.module.employee.entity.LeaveRequest;
import swari.sewa.module.employee.entity.SalaryRecord;
import swari.sewa.module.employee.repository.AttendanceRepository;
import swari.sewa.module.employee.repository.EmployeeRepository;
import swari.sewa.module.employee.repository.AdvancePaymentRepository;
import swari.sewa.module.employee.repository.LeaveRequestRepository;
import swari.sewa.module.employee.repository.SalaryRecordRepository;

@Component("employeeSecurity")
@RequiredArgsConstructor
public class EmployeeSecurity {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AdvancePaymentRepository advancePaymentRepository;

    /**
     * Check if the authenticated shop owner (by email) owns the employee with the given employee ID.
     * 
     * @param employeeId The employee ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the employee, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEmployeeOwner(Long employeeId, String email) {
        if (employeeId == null || email == null) {
            return false;
        }

        try {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null || employee.getShop() == null) {
                return false;
            }

            // Compare the shop owner's email with the authenticated email
            return email.equals(employee.getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the attendance record with the given ID.
     * 
     * @param attendanceId The attendance ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the attendance record, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAttendanceOwner(Long attendanceId, String email) {
        if (attendanceId == null || email == null) {
            return false;
        }

        try {
            Attendance attendance = attendanceRepository.findById(attendanceId).orElse(null);
            if (attendance == null || attendance.getEmployee() == null || attendance.getEmployee().getShop() == null) {
                return false;
            }

            return email.equals(attendance.getEmployee().getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the salary record with the given ID.
     * 
     * @param salaryId The salary record ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the salary record, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isSalaryOwner(Long salaryId, String email) {
        if (salaryId == null || email == null) {
            return false;
        }

        try {
            SalaryRecord salary = salaryRecordRepository.findById(salaryId).orElse(null);
            if (salary == null || salary.getEmployee() == null || salary.getEmployee().getShop() == null) {
                return false;
            }

            return email.equals(salary.getEmployee().getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the leave request with the given ID.
     * 
     * @param leaveId The leave request ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the leave request, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isLeaveOwner(Long leaveId, String email) {
        if (leaveId == null || email == null) {
            return false;
        }

        try {
            LeaveRequest leave = leaveRequestRepository.findById(leaveId).orElse(null);
            if (leave == null || leave.getEmployee() == null || leave.getEmployee().getShop() == null) {
                return false;
            }

            return email.equals(leave.getEmployee().getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the authenticated shop owner (by email) owns the advance payment with the given ID.
     * 
     * @param advanceId The advance payment ID to check ownership against
     * @param email The authenticated user's email (from authentication.getName())
     * @return true if the authenticated shop owner owns the advance payment, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAdvanceOwner(Long advanceId, String email) {
        if (advanceId == null || email == null) {
            return false;
        }

        try {
            AdvancePayment advance = advancePaymentRepository.findById(advanceId).orElse(null);
            if (advance == null || advance.getEmployee() == null || advance.getEmployee().getShop() == null) {
                return false;
            }

            return email.equals(advance.getEmployee().getShop().getShopOwner().getEmail());
        } catch (Exception e) {
            return false;
        }
    }
}
