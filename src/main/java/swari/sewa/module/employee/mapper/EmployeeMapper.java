package swari.sewa.module.employee.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import swari.sewa.module.employee.dto.EmployeeDto;
import swari.sewa.module.employee.dto.EmployeeRequestDto;
import swari.sewa.module.employee.dto.EmployeeUpdateDto;
import swari.sewa.module.employee.entity.Employee;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployeeMapper {
    
    EmployeeDto toDto(Employee employee);
    
    Employee toEntity(EmployeeDto employeeDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Employee toEntity(EmployeeRequestDto requestDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeNumber", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(EmployeeDto dto, @MappingTarget Employee employee);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeNumber", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequestDto(EmployeeRequestDto requestDto, @MappingTarget Employee employee);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeNumber", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromUpdateDto(EmployeeUpdateDto updateDto, @MappingTarget Employee employee);
}
