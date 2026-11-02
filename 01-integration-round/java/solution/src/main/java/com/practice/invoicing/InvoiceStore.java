package com.practice.invoicing;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class InvoiceStore {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private InvoiceStore() {}

    public static List<Invoice> load(Path path) throws IOException {
        List<Invoice> raw = GSON.fromJson(Files.readString(path), new TypeToken<List<Invoice>>() {}.getType());
        return raw.stream()
                .map(i -> new Invoice(i.id(), i.customerId(), i.currency().toLowerCase(Locale.ROOT), i.status(), i.lineItems()))
                .toList();
    }
}
