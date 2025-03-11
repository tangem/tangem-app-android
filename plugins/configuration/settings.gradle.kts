dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("../../gradle/dependencies.toml"))
        }
        create("tangemDeps") {
            from(files("../../gradle/tangem_dependencies.toml"))
        }
    }
}