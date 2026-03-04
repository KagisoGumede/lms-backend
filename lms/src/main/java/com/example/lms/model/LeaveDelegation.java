package com.example.lms.model;



import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_delegations")
public class LeaveDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator; // manager who is delegating

    @ManyToOne
    @JoinColumn(name = "delegate_id", nullable = false)
    private User delegate; // manager who receives delegation

    @Column(nullable = false)
    private LocalDate expiryDate;

    private boolean active = true;

    public Long getId() { return id; }

    public User getDelegator() { return delegator; }
    public void setDelegator(User delegator) { this.delegator = delegator; }

    public User getDelegate() { return delegate; }
    public void setDelegate(User delegate) { this.delegate = delegate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}