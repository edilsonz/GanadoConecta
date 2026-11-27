module.exports = {
  testEnvironment: 'node',
  testMatch: ['<rootDir>/tests/jest/**/*.jest.cjs'],
  transform: { '^.+\\.ts$': '<rootDir>/tests/jest/typescript.cjs' },
  collectCoverageFrom: [
    'src/features/ganado/formularioAnimal.ts',
    'src/shared/api/solicitudPrivada.ts',
  ],
  coverageDirectory: '<rootDir>/coverage/jest',
  coverageReporters: ['text-summary', 'json-summary', 'lcov'],
}
