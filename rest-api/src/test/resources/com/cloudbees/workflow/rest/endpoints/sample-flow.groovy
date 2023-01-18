package view.run_input_required

node {
  def outcome
  stage('S1') {
    // Define an input step and capture the outcome from it.
    outcome = input id: 'Run-test-suites',
          message: 'Workflow Configuration',
          ok: 'Okay',
          parameters: [
          [ 
            $class: 'BooleanParameterDefinition',
            defaultValue: true,
            name: 'Run test suites?',
            description: 'A checkbox option'
          ],
          [ 
            $class: 'StringParameterDefinition',
            defaultValue: "Hello",
            name: 'Enter some text',
            description: 'A text option'
          ],
          [
            $class: 'ChoiceParameterDefinition', choices: 'Choice 1\nChoice 2\nChoice 3', 
            name: 'Take your pick',
            description: 'A select box option'
          ]
    ]  
  }
  stage('S2') {
    // Echo the outcome values so they can be checked fro in the test. This will help
    // verify that input submit/proceed worked properly.
    echo "P1: ${outcome.get('Run test suites?')}"
    echo "P2: ${outcome.get('Enter some text')}"
    echo "P4: ${outcome.get('Take your pick')}"
  }
}
