all: run


build/install/ktor-grpc-sample/bin/ktor-grpc-sample: 
	./gradlew installDist

run: build/install/ktor-grpc-sample/bin/ktor-grpc-sample
	build/install/ktor-grpc-sample/bin/ktor-grpc-sample

clean:
	rm -rf build/
