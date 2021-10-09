.PHONY: install re clean

install:
	mvn -Dmaven.repo.local=.m2/repository -T 1C install

re:
	mvn -Dmaven.repo.local=.m2/repository -T 1C clean install

clean:
	mvn -Dmaven.repo.local=.m2/repository -T 1C clean
