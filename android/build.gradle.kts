plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}

// AGP 9.4.1 still resolves these on its build classpath. Force the patched
// versions so Dependabot's plugin graph is not left on known-vulnerable jars.
buildscript {
    dependencies {
        classpath("org.bouncycastle:bcprov-jdk18on:1.85")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
        classpath("org.bouncycastle:bcutil-jdk18on:1.85")
        classpath("org.bitbucket.b_c:jose4j:0.9.6")
        classpath("org.jdom:jdom2:2.0.6.1")
    }
}
