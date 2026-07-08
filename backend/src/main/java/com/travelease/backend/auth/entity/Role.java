package com.travelease.backend.auth.entity;

public enum Role {
    ROLE_ADMIN,

    /**
     * Travel/Bus transport provider only - does not grant Hotel or Activity
     * provider access. Bus Provider, Hotel Provider, and Activity Provider are
     * separate business actors in TravelEase; see ROLE_HOTEL_PROVIDER for the
     * accommodation-side actor.
     */
    ROLE_PROVIDER,

    /**
     * Hotel/Accommodation provider only - does not grant transport (ROLE_PROVIDER)
     * or Activity provider access.
     */
    ROLE_HOTEL_PROVIDER,

    /**
     * Activity/Experience provider only - does not grant transport (ROLE_PROVIDER)
     * or Hotel (ROLE_HOTEL_PROVIDER) provider access. Bus Provider, Hotel
     * Provider, and Activity Provider are separate business actors.
     */
    ROLE_ACTIVITY_PROVIDER,

    ROLE_TRAVELER
}
