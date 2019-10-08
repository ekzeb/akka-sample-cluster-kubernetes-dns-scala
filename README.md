# akka-sample-cluster-kubernetes-dns-scala
> fork of lightbend [akka-sample-cluster-kubernetes-dns-java](https://github.com/akka/akka-sample-cluster-kubernetes-dns-java)
### Run
- [install minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/)
- #### Local image 
  - `minikube start && eval $(minikube docker-env) `
  - `sbt docker:publishLocal`
- #### Remote image
  - set `dockerUsername := Some(DOCKER_REG_USER_NAME) & ` 
  - change ./kubernetes/akka-cluster.yml:Deployment:spec:template:spec:containers:image -> `DOCKER_REG_USER_NAME`/akka-sample-cluster-kubernetes-dns-scala:TAG
  - if not docker hub, set `dockerRepository := Some(DOCKER_REG_URL)`
  - `sbt docker:publish`
  - comment out ./kubernetes/akka-cluster.yml:Deployment:spec:template:spec:imagePullPolicy: Never
  
  
- `./kube-create.sh`
- `minikube service akka-sample-cluster-kubernetes-dns-scala`

### Check 
- `curl http:port/events` etc
- `curl management:port/cluster/members` etc

