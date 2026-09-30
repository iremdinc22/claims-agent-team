import { type FormEvent, useState } from 'react'
import { ClaimsApiError, createClaim } from './claimsApi'
import {
  INCIDENT_TYPES,
  type CreateClaimRequest,
  type IncidentType,
} from './types'

type FormValues = {
  policyNumber: string
  incidentType: IncidentType | ''
  incidentDate: string
  description: string
}

type FieldName = keyof FormValues
type FieldErrors = Partial<Record<FieldName, string>>

const EMPTY_FORM: FormValues = {
  policyNumber: '',
  incidentType: '',
  incidentDate: '',
  description: '',
}

const INCIDENT_LABELS: Record<IncidentType, string> = {
  COLLISION: 'Collision',
  THEFT: 'Theft',
  GLASS_DAMAGE: 'Glass damage',
  OTHER: 'Other',
}

const FIELD_NAMES = new Set<FieldName>([
  'policyNumber',
  'incidentType',
  'incidentDate',
  'description',
])

function localIsoDate(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function validate(values: FormValues): FieldErrors {
  const errors: FieldErrors = {}

  if (!values.policyNumber.trim()) {
    errors.policyNumber = 'Policy number is required.'
  }

  if (!values.incidentType) {
    errors.incidentType = 'Incident type is required.'
  }

  if (!values.incidentDate) {
    errors.incidentDate = 'Incident date is required.'
  } else if (values.incidentDate > localIsoDate()) {
    errors.incidentDate = 'Incident date cannot be in the future.'
  }

  if (!values.description.trim()) {
    errors.description = 'Description is required.'
  }

  return errors
}

function supportedFieldErrors(fieldErrors: Record<string, string>): FieldErrors {
  return Object.fromEntries(
    Object.entries(fieldErrors).filter(([field]) =>
      FIELD_NAMES.has(field as FieldName),
    ),
  ) as FieldErrors
}

export default function CreateClaimForm() {
  const [values, setValues] = useState<FormValues>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [claimNumber, setClaimNumber] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (field: FieldName, value: string) => {
    setValues((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
    setGeneralError(null)
    setClaimNumber(null)
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    const clientErrors = validate(values)
    setFieldErrors(clientErrors)
    setGeneralError(null)
    setClaimNumber(null)

    if (Object.keys(clientErrors).length > 0) {
      return
    }

    const request: CreateClaimRequest = {
      policyNumber: values.policyNumber,
      incidentType: values.incidentType as IncidentType,
      incidentDate: values.incidentDate,
      description: values.description,
    }

    setIsSubmitting(true)

    try {
      const response = await createClaim(request)
      setValues(EMPTY_FORM)
      setFieldErrors({})
      setClaimNumber(response.claimNumber)
    } catch (error) {
      if (error instanceof ClaimsApiError && error.response) {
        const backendFieldErrors = supportedFieldErrors(error.response.fieldErrors)
        setFieldErrors(backendFieldErrors)

        const hasFieldErrors = Object.keys(backendFieldErrors).length > 0
        if (!hasFieldErrors || error.response.code === 'INTERNAL_ERROR') {
          setGeneralError(error.response.message)
        }
      } else {
        setGeneralError(
          'We could not create your claim. Please check your connection and try again.',
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const inputErrorProps = (field: FieldName) => ({
    'aria-invalid': Boolean(fieldErrors[field]),
    'aria-describedby': fieldErrors[field] ? `${field}-error` : undefined,
  })

  return (
    <section className="claim-card" aria-labelledby="claim-form-title">
      <div className="card-heading">
        <div>
          <p className="step-label">New claim</p>
          <h2 id="claim-form-title">Incident details</h2>
        </div>
        <span className="required-note">All fields are required</span>
      </div>

      {claimNumber && (
        <div className="alert alert-success" role="status">
          <strong>Claim created successfully.</strong>
          <span>
            Your claim number is <b>{claimNumber}</b>.
          </span>
        </div>
      )}

      {generalError && (
        <div className="alert alert-error" role="alert">
          <strong>Claim could not be created.</strong>
          <span>{generalError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          <div className="field-group field-span-full">
            <label htmlFor="policyNumber">Policy number</label>
            <input id="policyNumber" name="policyNumber" type="text" value={values.policyNumber} onChange={(event) => updateField('policyNumber', event.target.value)} disabled={isSubmitting} required autoComplete="off" placeholder="e.g. MOTOR-POLICY-001" {...inputErrorProps('policyNumber')} />
            {fieldErrors.policyNumber && <p className="field-error" id="policyNumber-error">{fieldErrors.policyNumber}</p>}
          </div>

          <div className="field-group">
            <label htmlFor="incidentType">Incident type</label>
            <select id="incidentType" name="incidentType" value={values.incidentType} onChange={(event) => updateField('incidentType', event.target.value)} disabled={isSubmitting} required {...inputErrorProps('incidentType')}>
              <option value="">Select an incident type</option>
              {INCIDENT_TYPES.map((type) => <option key={type} value={type}>{INCIDENT_LABELS[type]}</option>)}
            </select>
            {fieldErrors.incidentType && <p className="field-error" id="incidentType-error">{fieldErrors.incidentType}</p>}
          </div>

          <div className="field-group">
            <label htmlFor="incidentDate">Incident date</label>
            <input id="incidentDate" name="incidentDate" type="date" max={localIsoDate()} value={values.incidentDate} onChange={(event) => updateField('incidentDate', event.target.value)} disabled={isSubmitting} required {...inputErrorProps('incidentDate')} />
            {fieldErrors.incidentDate && <p className="field-error" id="incidentDate-error">{fieldErrors.incidentDate}</p>}
          </div>

          <div className="field-group field-span-full">
            <label htmlFor="description">Description</label>
            <textarea id="description" name="description" rows={5} value={values.description} onChange={(event) => updateField('description', event.target.value)} disabled={isSubmitting} required placeholder="Describe what happened" {...inputErrorProps('description')} />
            {fieldErrors.description && <p className="field-error" id="description-error">{fieldErrors.description}</p>}
          </div>
        </div>

        <div className="form-actions">
          <p>Submitting records the claim as reported for subsequent handling.</p>
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating claim…' : 'Create claim'}
          </button>
        </div>
      </form>
    </section>
  )
}
