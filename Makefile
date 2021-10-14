.PHONY: install re clean

install: gensrc
	mvn -Dmaven.repo.local=.m2/repository -T 1C install

gensrc: src/main/proto/builds.proto
	rm -rf gensrc
	mkdir -p gensrc/main/java
	protoc -Isrc/main/proto --java_out=gensrc/main/java src/main/proto/builds.proto

re:
	mvn -Dmaven.repo.local=.m2/repository -T 1C clean install

clean:
	mvn -Dmaven.repo.local=.m2/repository -T 1C clean
