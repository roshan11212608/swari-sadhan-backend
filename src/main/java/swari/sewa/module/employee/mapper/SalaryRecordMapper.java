package swari.sewa.module.employee.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import swari.sewa.module.employee.dto.SalaryRecordDto;
import swari.sewa.module.employee.entity.SalaryRecord;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SalaryRecordMapper {
    
    @Mapping(source = "employee.id", target = "employeeId")
    SalaryRecordDto toDto(SalaryRecord salaryRecord);
    
    SalaryRecord toEntity(SalaryRecordDto salaryRecordDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    void updateEntityFromDto(SalaryRecordDto dto, @MappingTarget SalaryRecord salaryRecord);
}
