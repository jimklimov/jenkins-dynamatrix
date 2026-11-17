def call() {
    println "Class: ${this.class.name}"
    println "Loader: ${this.class.classLoader}"
    println "Source: ${this.class.protectionDomain?.codeSource?.location}"

    echo 'library echo'
    node {
        sh 'echo library sh'
    }
}

