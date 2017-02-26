# Detecting flaky test with sbt

## Introduction
This project is proof of concept of flaky test detector SBT plugin. It can run test X times, for X minutes or until first failure.

## How to run

Run tests 30 times:
```
sbt clean "flaky times=30"
```

Run tests 30 minutes:
```
sbt clean "flaky duration=30"
```


Run tests until first failure:
```
sbt clean "flaky firstFail"
```

## Sending reports to slack
To send report to Slack, set SLACK_HOOKID variable with your Slack hook id. For example if your slack hook is `https://hooks.slack.com/services/AAAAAAAAA/BBBBBBBBB/CCCCCCCCCCCCCCCCCCCCCCCC `, run sbt with command line like this:
`sbt -DSLACK_HOOKID=https://hooks.slack.com/services/AAAAAAAAA/BBBBBBBBB/CCCCCCCCCCCCCCCCCCCCCCCC clean "flaky times=1"`

## How it works.
Command `flaky` execute `test` task mulitplie times. After every test iteration, test results from `./target/test-reports` is moved to `./target/flaky-report/<ITERATION>`. Test taks is run for X times, for X minutes or untill first failing test task. All tests results are used to calculate success ratio for every test.

## Example report

Currently only simple test report is printed:
```
Healthy tests:
A ForwardingActor should Forwards in a huge chain
A ForwardingActor should Forward a message it receives
A Stack should pop values in last-in-first-out order
A Stack should throw NoSuchElementException if an empty stack is popped

Flaky tests:
A Stack should fail sometimes 20%
A Stack should fail randomly often 7%
A ForwardingActor should Forwards in a 2 huge chains 7%
A Stack should fail randomly 6%
A Stack should fail randomly sometimes 1%

Details:
ExampleSpec: A Stack should fail sometimes failed in runs: 100, 101, 104, 105, 106, 108, 116, 117, 122, 123, 125, 129, 13, 131, 134, 148, 168, 171, 172, 174, 18, 181, 183, 185, 191, 192, 198, 199, 217, 218, 219, 22, 222, 223, 227, 229, 235, 236, 239, 241, 242, 246, 252, 253, 260, 275, 279, 30, 41, 42, 59, 6, 78, 79, 81, 89, 92
ExampleSpec: A Stack should fail randomly often failed in runs: 10, 136, 143, 170, 172, 20, 21, 239, 243, 253, 259, 26, 268, 28, 284, 4, 57, 77, 82, 94, 96
TestKitUsageSpec: A ForwardingActor should Forwards in a 2 huge chains failed in runs: 10, 118, 13, 143, 199, 21, 22, 228, 229, 23, 256, 264, 266, 271, 284, 49, 66, 67, 79, 85, 89
ExampleSpec: A Stack should fail randomly failed in runs: 119, 120, 154, 160, 169, 186, 196, 20, 229, 230, 235, 240, 262, 263, 5, 58, 65, 72, 86
ExampleSpec: A Stack should fail randomly sometimes failed in runs: 113, 141, 160, 225, 283
```

## Known issues

If running a lot of tests for a many times you can get Out of memory error: `java.lang.OutOfMemoryError: Metaspace`. On report you will find flay test `(It is not a test)`.
Try to tune JVM memory settings: `-XX:MaxMetaspaceSize=512m`
For example env `JAVA_OPTS="-XX:MaxMetaspaceSize=512m" sbt "flaky times=400"`

Additionally, you can run tests in separate JVM


# TODO
- [x] Run tests X times
- [x] Run tests for X minutes
- [x] Run test until first failure
- [x] Copy log file to run test iteration dir
- [x] Execute webhook after tests (slack)
- [ ] Create SBT plugin
- [ ] Generating report (HTML, XML or JSON)
- [ ] Select single test (or test class) to run (like testOnly task)
- [ ] Update readme after migration to plugin
