package swari.sewa.common.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class MapperUtil {
    
    private static final ModelMapper modelMapper = new ModelMapper();
    
    public static <D, T> D mapToEntity(T source, Class<D> destinationType) {
        return modelMapper.map(source, destinationType);
    }
    
    public static <D, T> D mapToDto(T source, Class<D> destinationType) {
        return modelMapper.map(source, destinationType);
    }
    
    public static ModelMapper getModelMapper() {
        return modelMapper;
    }
}
