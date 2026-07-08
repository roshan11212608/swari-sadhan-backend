package swari.sewa.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import swari.sewa.module.vehicle.dto.VehicleDto;
import swari.sewa.module.vehicle.entity.Vehicle;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setAmbiguityIgnored(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        modelMapper.typeMap(VehicleDto.class, Vehicle.class)
                .addMappings(mapper -> mapper.skip(Vehicle::setType))
                .addMappings(mapper -> mapper.map(VehicleDto::getMainImageUrl, Vehicle::setMainImageUrl))
                .addMappings(mapper -> mapper.map(VehicleDto::getImageUrls, Vehicle::setImageUrls))
                .addMappings(mapper -> mapper.map(VehicleDto::getSellerPassportPhoto, Vehicle::setSellerPassportPhoto))
                .addMappings(mapper -> mapper.map(VehicleDto::getSellerCitizenshipFront, Vehicle::setSellerCitizenshipFront))
                .addMappings(mapper -> mapper.map(VehicleDto::getSellerCitizenshipBack, Vehicle::setSellerCitizenshipBack));

        return modelMapper;
    }
}
