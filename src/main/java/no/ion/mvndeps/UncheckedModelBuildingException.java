package no.ion.mvndeps;

import org.apache.maven.model.building.ModelBuildingException;

public class UncheckedModelBuildingException extends RuntimeException {
    public UncheckedModelBuildingException(ModelBuildingException cause) {
        super(cause);
    }
}
