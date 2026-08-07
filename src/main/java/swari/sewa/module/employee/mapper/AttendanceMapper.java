package swari.sewa.module.employee.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import swari.sewa.module.employee.dto.AttendanceDto;
import swari.sewa.module.employee.dto.AttendanceRequestDto;
import swari.sewa.module.employee.entity.Attendance;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttendanceMapper {
    
    @Mapping(source = "employee.id", target = "employeeId")
    AttendanceDto toDto(Attendance attendance);
    
    Attendance toEntity(AttendanceDto attendanceDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Attendance toEntity(AttendanceRequestDto requestDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(AttendanceDto dto, @MappingTarget Attendance attendance);
}
