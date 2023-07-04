package de.davis.net.sync;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.davis.net.gson.TypeAdapter;
import de.davis.net.handlers.ErrorHandler;
import de.davis.net.sync.models.Model;

@SuppressWarnings("unchecked")
public abstract class AbstractBuilder<B extends AbstractBuilder<B, C>, C extends Client> {

    protected ErrorHandler errorHandler;

    protected Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Model.class, new TypeAdapter())
            .create();

    public B withErrorHandler(ErrorHandler errorHandler){
        this.errorHandler = errorHandler;
        return (B) this;
    }

    public B withGson(Gson gson){
        this.gson = gson;
        return (B) this;
    }

    public abstract C build();
}
