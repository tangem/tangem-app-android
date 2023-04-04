dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("../../gradle/dependencies.toml"))
        }
    }
}
