def call() {
    println "Class: ${this.class.name}"
    println "Loader: ${this.class.classLoader}"
    println "Source: ${this.class.protectionDomain?.codeSource?.location}"

    return [
        className: this.class.name,
        superclass: this.class.superclass.name,
        classLoader: this.class.classLoader.toString(),
        source: this.class.protectionDomain?.codeSource?.location?.toString()
    ]
}

